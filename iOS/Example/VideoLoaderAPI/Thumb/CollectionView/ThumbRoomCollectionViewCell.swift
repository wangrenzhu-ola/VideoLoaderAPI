//
//  ThumbRoomCollectionViewCell.swift
//  TestVideoLoader
//
//  Created by wushengtao on 2023/8/30.
//

import UIKit

class ThumbRoomCollectionViewCell: UICollectionViewCell {
    public lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.numberOfLines = 0
        label.textColor = .blue
        label.textAlignment = .right
        label.font = UIFont.boldSystemFont(ofSize: 16)
        return label
    }()
    
    public lazy var canvasView: UIView = UIView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        _loadSubview()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func _loadSubview() {
        contentView.backgroundColor = .red
        canvasView.backgroundColor = .yellow
        contentView.addSubview(canvasView)
        contentView.addSubview(titleLabel)
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        titleLabel.sizeToFit()
        canvasView.frame = CGRect(x: 40, y: 100, width: 200, height: 300)
        titleLabel.frame = CGRect(x: frame.width - titleLabel.frame.width - 10, y: 80, width: titleLabel.bounds.width, height: titleLabel.bounds.height)
    }
}
